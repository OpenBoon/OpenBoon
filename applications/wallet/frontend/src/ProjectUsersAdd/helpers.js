import { fetcher } from '../Fetch/helpers'

const userMapper = ({ users }) => {
  const mappedUsers = users.reduce((acc, user) => {
    const { email, permissions } = user

    acc.push({ email, permissions })

    return acc
  }, [])

  return mappedUsers
}

export const onSubmit = async ({
  projectId,
  dispatch,
  state: { emails: e, permissions: p },
}) => {
  try {
    const emails = e.split(',').map(str => str.trim(''))
    const permissions = Object.keys(p).filter(name => p[name])
    const body = JSON.stringify({
      batch: emails.map(email => ({ email, permissions })),
    })

    const {
      results: { succeeded, failed },
    } = await fetcher(`/api/v1/projects/${projectId}/users/`, {
      method: 'POST',
      body,
    })

    dispatch({
      succeeded: userMapper({ users: succeeded }),
      failed: userMapper({ users: failed }),
    })
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ errors: parsedErrors })
  }
}
