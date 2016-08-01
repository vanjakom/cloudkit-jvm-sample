(ns cloudkit-jvm-sample.sample)

(require '[clj-http.client :as http])
(require '[clojure.data.json :as json])

(defn create-signature-fn [private-key-hex]
  (let [keypair-generator (java.security.KeyPairGenerator/getInstance "EC")
        prime256v1-gen-param-spec (new java.security.spec.ECGenParameterSpec "secp256r1")]
    (.initialize keypair-generator prime256v1-gen-param-spec)
    (let [ec-params (.getParams (.getPrivate (.generateKeyPair keypair-generator)))
          private-key-bigint (new java.math.BigInteger private-key-hex 16)
          private-key-specs (new java.security.spec.ECPrivateKeySpec private-key-bigint ec-params)
          key-factory (java.security.KeyFactory/getInstance "EC")
          private-key (.generatePrivate key-factory private-key-specs)
          signature (java.security.Signature/getInstance "SHA256withECDSA")
          base64-encoder (java.util.Base64/getEncoder)]
      (.initSign signature private-key)
      (fn [body]
        (.update signature (.getBytes body))
        (.encodeToString base64-encoder (.sign signature))))))

(defn user-request [private-key-hex key-id cloudkit-container]
  (let [simple-date-formatter (new java.text.SimpleDateFormat "yyyy-MM-dd'T'HH:mm:ss'Z'")
        timezone (java.util.TimeZone/getTimeZone "UTC")
        base64-encoder (java.util.Base64/getEncoder)
        sha256-digest (java.security.MessageDigest/getInstance "SHA-256")
        signature-fn (create-signature-fn private-key-hex)]
    (.setTimeZone simple-date-formatter timezone)
    (let [request-date-iso (.format simple-date-formatter (new java.util.Date))
          subpath (str "/database/1/" cloudkit-container "/development/public/users/current")
          body ""
          body-sha256 (.digest sha256-digest (.getBytes body))
          body-sha256-base64 (.encodeToString base64-encoder body-sha256)
          sign-request (str request-date-iso ":" body-sha256-base64 ":" subpath)
          signature (signature-fn sign-request)
          headers {
                    "X-Apple-CloudKit-Request-KeyID" key-id
                    "X-Apple-CloudKit-Request-ISO8601Date" request-date-iso
                    "X-Apple-CloudKit-Request-SignatureV1" signature}]
      (let [response (http/get
                       (str "https://api.apple-cloudkit.com" subpath)
                       {
                         :headers headers
                         :insecure? true})]
        (json/read-str (:body response))))))
