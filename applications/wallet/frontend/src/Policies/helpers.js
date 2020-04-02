import { fetcher } from '../Fetch/helpers'

/**
 * How to update legal documents:
 * 1. Update the CURRENT_POLICIES_DATE constant below
 * 2. Create a new folder `/public/policies/${CURRENT_POLICIES_DATE}/`
 * 3. Add both T&C and PP new files in that folder, even if only one has changed
 * 4. Update `agreedToPoliciesDate` in `src/User/__mocks__/user.js`
 * 5. Update exactly 3 snapshots, where only CURRENT_POLICIES_DATE changes
 */

export const CURRENT_POLICIES_DATE = '20200626'

export const onSubmit = async ({ dispatch, userId, setUser }) => {
  try {
    await fetcher(`/api/v1/users/${userId}/agreements/`, {
      method: 'POST',
      body: JSON.stringify({ policies_date: CURRENT_POLICIES_DATE }),
    })

    setUser({ user: { agreedToPoliciesDate: CURRENT_POLICIES_DATE } })
  } catch (response) {
    dispatch({ errors: { global: 'Something went wrong. Please try again.' } })
  }
}
