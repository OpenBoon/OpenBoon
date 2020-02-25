import { fetcher } from '../Fetch/helpers'

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

    dispatch({ succeeded, failed })
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ errors: parsedErrors })
  }
}

export const onCopy = ({ inputRef }) => {
  inputRef.current.select()
  document.execCommand('copy')
  inputRef.current.blur()
}
