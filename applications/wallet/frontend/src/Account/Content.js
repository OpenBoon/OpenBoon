import useSWR from 'swr'

import { spacing } from '../Styles'

import NoProject from '../NoProject'

import AccountCard from './Card'

const AccountContent = () => {
  const {
    data: { results: projects, count },
  } = useSWR('/api/v1/projects/')

  if (projects.length === 0) {
    return <NoProject />
  }

  return (
    <>
      <h3 css={{ paddingTop: spacing.normal, paddingBottom: spacing.normal }}>
        Number of Projects: {count}
      </h3>
      <div css={{ display: 'flex', flexWrap: 'wrap' }}>
        {projects.map(({ id: projectId, name }) => (
          <AccountCard key={projectId} projectId={projectId} name={name} />
        ))}
      </div>
    </>
  )
}

export default AccountContent
