import { mutate } from 'swr'

import { fetcher, parseResponse } from '../Fetch/helpers'

/**
 * How to update legal documents:
 * 1. Update the CURRENT_POLICIES_DATE constant below
 * 2. Create a new folder `/public/policies/${CURRENT_POLICIES_DATE}/`
 *    leaving older folders intact.
 * 3. Add both `terms-of-use.pdf` and `privacy-policy.pdf` new files
 *    in that folder, even if only one has changed
 * 4. Update exactly 5 snapshots failed from 3 test suites,
 *    where only CURRENT_POLICIES_DATE changes
 */

export const CURRENT_POLICIES_DATE = '20210211'

export const onSubmit = async ({ dispatch }) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`/api/v1/me/agreements/`, {
      method: 'POST',
      body: JSON.stringify({ policiesDate: CURRENT_POLICIES_DATE }),
    })

    mutate(
      '/api/v1/me/',
      (user) => ({ ...user, agreedToPoliciesDate: CURRENT_POLICIES_DATE }),
      false,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
