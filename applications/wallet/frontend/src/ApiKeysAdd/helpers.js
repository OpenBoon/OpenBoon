import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, permissions: p },
}) => {
  try {
    const permissions = Object.keys(p).filter(key => p[key])

    const apikey = await fetcher(`/api/v1/projects/${projectId}/apikeys/`, {
      method: 'POST',
      body: JSON.stringify({ name, permissions }),
    })

    dispatch({ apikey })
  } catch (response) {
    const errors = await response.json()

    const parsedErrors = Object.keys(errors).reduce((accumulator, errorKey) => {
      accumulator[errorKey] = errors[errorKey].join(' ')
      return accumulator
    }, {})

    dispatch({ errors: parsedErrors })
  }
}

export const onCopy = ({ textareaRef }) => {
  textareaRef.current.select()
  document.execCommand('copy')
  textareaRef.current.blur()
}
