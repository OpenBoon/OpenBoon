/**
 * console
 */
const { error } = console

console.error = (message, ...args) => {
  error.apply(console, [message, ...args])
  throw new Error('Please fix console.error below')
}
