export const getJobId = ({ url }) => {
  const projects = url.indexOf('projects')
  return url.substr(projects + 9).split('/')[0]
}
