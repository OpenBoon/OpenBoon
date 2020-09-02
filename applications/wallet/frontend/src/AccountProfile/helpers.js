import { mutate } from 'swr'

import { fetcher, parseResponse } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  state: { firstName, lastName },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`/api/v1/me/`, {
      method: 'PATCH',
      body: JSON.stringify({ firstName, lastName }),
    })

    dispatch({
      showForm: false,
      success: true,
      isLoading: false,
      errors: {},
    })

    mutate('/api/v1/me/', (user) => ({ ...user, firstName, lastName }), false)
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ success: false, isLoading: false, errors })
  }
}
