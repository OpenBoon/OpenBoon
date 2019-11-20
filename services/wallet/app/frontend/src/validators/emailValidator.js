const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function emailValidator(email) {
  return EMAIL_REGEX.test(email)
}

export default emailValidator