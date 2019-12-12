export const parseId = ({ url }) => {
  const lastSlash = url.lastIndexOf('/')
  return url.substr(lastSlash + 1)
}
