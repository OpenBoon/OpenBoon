import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  projectId,
  dispatch,
  state: { emails: e, permissions: p },
}) => {
  try {
    const emails = e.split(',').map(str => str.trim(''))
    const permissions = Object.keys(p).filter(name => p[name])
    let body

    if (emails.length > 1) {
      body = JSON.stringify({
        batch: emails.map(email => ({ email, permissions })),
      })
    }

    if (emails.length === 1) {
      body = JSON.stringify({ email: emails, permissions })
    }

    await fetcher(`api/v1/projects/${projectId}/users/`, {
      method: 'POST',
      body,
    })

    dispatch({ success: true })
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ success: false, errors: parsedErrors })
  }
}
