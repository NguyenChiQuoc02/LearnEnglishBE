package com.personal.base.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Builds MoMo's "create payment" (AIO/QR captureWallet) request and verifies IPN callbacks,
// following the same RestTemplate + JsonNode pattern as ZaloOaService.
@Service
public class MomoService {

  private static final Logger log = LoggerFactory.getLogger(MomoService.class);
  private static final String REQUEST_TYPE = "captureWallet";
  private static final String HMAC_SHA256 = "HmacSHA256";

  @Autowired
  private RestTemplate restTemplate;

  @Value("${momo.partnerCode}")
  private String partnerCode;

  @Value("${momo.accessKey}")
  private String accessKey;

  @Value("${momo.secretKey}")
  private String secretKey;

  @Value("${momo.endpoint}")
  private String endpoint;

  @Value("${momo.ipnUrl}")
  private String ipnUrl;

  @Value("${momo.redirectUrl}")
  private String redirectUrl;

  public MomoCreateResult createPayment(long amountVnd, String orderInfo) {
    String requestId = UUID.randomUUID().toString();
    String orderId = UUID.randomUUID().toString();
    String extraData = "";
    String amount = String.valueOf(amountVnd);

    // Exact field order per MoMo's documented captureWallet signature spec.
    String rawSignature = "accessKey=" + accessKey
            + "&amount=" + amount
            + "&extraData=" + extraData
            + "&ipnUrl=" + ipnUrl
            + "&orderId=" + orderId
            + "&orderInfo=" + orderInfo
            + "&partnerCode=" + partnerCode
            + "&redirectUrl=" + redirectUrl
            + "&requestId=" + requestId
            + "&requestType=" + REQUEST_TYPE;

    String signature = hmacSha256(secretKey, rawSignature);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("partnerCode", partnerCode);
    body.put("partnerName", "LearnEnglish");
    body.put("storeId", "LearnEnglishStore");
    body.put("requestId", requestId);
    body.put("amount", amount);
    body.put("orderId", orderId);
    body.put("orderInfo", orderInfo);
    body.put("redirectUrl", redirectUrl);
    body.put("ipnUrl", ipnUrl);
    body.put("lang", "vi");
    body.put("extraData", extraData);
    body.put("requestType", REQUEST_TYPE);
    body.put("signature", signature);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    JsonNode response;
    try {
      response = restTemplate.postForObject(endpoint, new HttpEntity<>(body, headers), JsonNode.class);
    } catch (Exception e) {
      log.warn("MoMo createPayment call failed", e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể kết nối tới MoMo");
    }

    if (response == null || !response.hasNonNull("payUrl")) {
      String message = response != null && response.hasNonNull("message") ? response.get("message").asText() : "MoMo did not return a payment URL";
      log.warn("MoMo createPayment did not return payUrl: {}", response);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    return new MomoCreateResult(orderId, response.get("payUrl").asText());
  }

  // MoMo's IPN body is a JSON object containing (among others) these fields plus "signature".
  // Recomputes the raw signature from the received fields, per MoMo's documented IPN field
  // order, and compares against the signature MoMo sent.
  // NOTE: implemented as closely as reasonably possible to MoMo's published IPN spec without
  // being able to consult their live docs right now — double check field order/names against
  // https://developers.momo.vn before relying on this in production.
  public boolean verifyIpnSignature(Map<String, String> params) {
    String receivedSignature = params.get("signature");
    if (receivedSignature == null) return false;

    String rawSignature = "accessKey=" + accessKey
            + "&amount=" + nullToEmpty(params.get("amount"))
            + "&extraData=" + nullToEmpty(params.get("extraData"))
            + "&message=" + nullToEmpty(params.get("message"))
            + "&orderId=" + nullToEmpty(params.get("orderId"))
            + "&orderInfo=" + nullToEmpty(params.get("orderInfo"))
            + "&orderType=" + nullToEmpty(params.get("orderType"))
            + "&partnerCode=" + nullToEmpty(params.get("partnerCode"))
            + "&payType=" + nullToEmpty(params.get("payType"))
            + "&requestId=" + nullToEmpty(params.get("requestId"))
            + "&responseTime=" + nullToEmpty(params.get("responseTime"))
            + "&resultCode=" + nullToEmpty(params.get("resultCode"))
            + "&transId=" + nullToEmpty(params.get("transId"));

    String expectedSignature = hmacSha256(secretKey, rawSignature);
    return expectedSignature.equalsIgnoreCase(receivedSignature);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String hmacSha256(String key, String data) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Unable to compute MoMo signature", e);
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MomoCreateResult {
    private String orderId;
    private String payUrl;
  }
}
