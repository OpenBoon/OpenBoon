UPDATE user SET hmac_key=RANDOM_UUID() WHERE str_username='admin';
