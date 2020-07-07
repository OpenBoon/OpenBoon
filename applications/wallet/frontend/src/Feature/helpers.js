export const parseFeatureFlags = ({ flags }) => {
  return flags
    .split(',')
    .map((flag) => {
      const [key, value] = flag.split(':')
      return { [key]: value === 'true' }
    })
    .reduce((acc, flag) => {
      return { ...acc, ...flag }
    }, {})
}
