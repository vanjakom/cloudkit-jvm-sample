
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.interfaces.ECKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;

public class Sample {
    public static void main(String[] args) throws Exception {
        String privateKeyHex = args[0];
        String keyId = args[1];
        String cloudKitContainer = args[2];

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        AlgorithmParameterSpec prime256v1ParamSpec = new ECGenParameterSpec("secp256r1");

        keyPairGenerator.initialize(prime256v1ParamSpec);

        ECParameterSpec parameterSpec = ((ECKey)keyPairGenerator.generateKeyPair().getPrivate()).getParams();

        BigInteger privateKeyInt = new BigInteger(privateKeyHex, 16);

        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKeyInt, parameterSpec);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        Signature signature = Signature.getInstance("SHA256withECDSA");

        signature.initSign(privateKey);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Base64.Encoder encoder = Base64.getEncoder();

        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");

        String requestDateIso = simpleDateFormat.format(new Date());
        String subPath = "/database/1/" + cloudKitContainer +  "/development/public/users/current";
        String body = "";
        byte[] bodySha256 = sha256Digest.digest(body.getBytes());
        String bodySha256Base64 = encoder.encodeToString(bodySha256);

        String signRequest = requestDateIso + ":" + bodySha256Base64 + ":" + subPath;
        signature.update(signRequest.getBytes());
        String signedRequest = encoder.encodeToString(signature.sign());

        URL url = new URL("https://api.apple-cloudkit.com" + subPath);

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setRequestMethod("GET");

        connection.setRequestProperty("X-Apple-CloudKit-Request-KeyID", keyId);
        connection.setRequestProperty("X-Apple-CloudKit-Request-ISO8601Date", requestDateIso);
        connection.setRequestProperty("X-Apple-CloudKit-Request-SignatureV1", signedRequest);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();
            System.out.println(response.toString());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}

