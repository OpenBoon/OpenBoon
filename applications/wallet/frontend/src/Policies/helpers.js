import { mutate } from 'swr'

import { fetcher } from '../Fetch/helpers'

/**
 * How to update legal documents:
 * 1. Update the CURRENT_POLICIES_DATE constant below
 * 2. Create a new folder `/public/policies/${CURRENT_POLICIES_DATE}/`
 *    leaving older folders intact.
 * 3. Add both `terms-of-use.pdf` and `privacy-policy.pdf` new files
 *    in that folder, even if only one has changed
 * 4. Update `agreedToPoliciesDate` in `src/User/__mocks__/user.js`
 * 5. Update <Policies /> test
 * 6. Update exactly 5 snapshots failed from 3 test suites,
 *    where only CURRENT_POLICIES_DATE changes
 */

export const CURRENT_POLICIES_DATE = '20200414'

export const onSubmit = async ({ dispatch, userId }) => {
  try {
    await fetcher(`/api/v1/users/${userId}/agreements/`, {
      method: 'POST',
      body: JSON.stringify({ policiesDate: CURRENT_POLICIES_DATE }),
    })

    mutate(
      '/api/v1/me/',
      (user) => ({ ...user, agreedToPoliciesDate: CURRENT_POLICIES_DATE }),
      false,
    )
  } catch (response) {
    dispatch({ errors: { global: 'Something went wrong. Please try again.' } })
  }
}
