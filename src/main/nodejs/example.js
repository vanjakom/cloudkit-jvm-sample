(function() {
	console.log("starting");

	var fs = require('fs');
	var crypto = require('crypto');

	var message = "test";

	var key = fs.readFileSync("key.pem", "utf8");

	var signature = crypto.createSign("sha256");
	signature.update(message);

	var signedMessage = signature.sign(key, "base64");

	console.log(signedMessage);
})();
