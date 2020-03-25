import useSWR from 'swr'

import { spacing } from '../Styles'

import NoProject from '../NoProject'

import AccountCards from './Cards'

const AccountContent = () => {
  const {
    data: { results, count },
  } = useSWR(`/api/v1/projects`)

  if (results.length === 0) {
    return <NoProject />
  }

  return (
    <>
      <h3 css={{ paddingTop: spacing.normal, paddingBottom: spacing.normal }}>
        Number of Projects: {count}
      </h3>
      <AccountCards projects={results} />
    </>
  )
}

export default AccountContent
