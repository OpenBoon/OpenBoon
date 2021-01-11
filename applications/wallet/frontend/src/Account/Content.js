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
      <div css={{ paddingTop: spacing.normal, paddingBottom: spacing.normal }}>
        Number of Projects: {count}
      </div>

      <div
        css={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: spacing.spacious,
          '@media screen and (min-width: 1500px)': {
            gridTemplateColumns: '1fr 1fr 1fr',
          },
          '@media screen and (min-width: 2000px)': {
            gridTemplateColumns: '1fr 1fr 1fr 1fr',
          },
        }}
      >
        {projects.map(({ id: projectId, name }) => (
          <AccountCard key={projectId} projectId={projectId} name={name} />
        ))}
      </div>
    </>
  )
}

export default AccountContent
