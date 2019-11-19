export default class User {
  constructor({ email, attrs = {} }) {
    this.email = email
    this.attrs = attrs
  }

  getAttr(attrName) {
    return this.attrs[attrName]
  }
}
