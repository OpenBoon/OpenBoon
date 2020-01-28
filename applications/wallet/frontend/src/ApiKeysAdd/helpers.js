import { fetcher } from '../Fetch/helpers'

export const onSubmit = ({ projectId }) => async ({ name, permissions: p }) => {
  const permissions = Object.keys(p).filter(key => p[key])

  await fetcher(`/api/v1/projects/${projectId}/apikeys/`, {
    method: 'POST',
    body: JSON.stringify({ name, permissions }),
  })
}
