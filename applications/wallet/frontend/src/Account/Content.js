import { useState } from 'react'
import useSWR from 'swr'

import { spacing } from '../Styles'

import NoProject from '../NoProject'
import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

import AccountCard from './Card'

const MIN_WIDTH = 400

const AccountContent = () => {
  const {
    data: { results: projects, count },
  } = useSWR('/api/v1/projects/')

  const [searchString, setSearchString] = useState('')

  if (projects.length === 0) {
    return <NoProject />
  }

  return (
    <>
      <div css={{ maxWidth: MIN_WIDTH + spacing.spacious * 4 }}>
        <InputSearch
          aria-label="Filter Projects"
          placeholder="Filter Projects"
          value={searchString}
          onChange={({ value }) => {
            setSearchString(value)
          }}
          variant={INPUT_SEARCH_VARIANTS.DARK}
        />
      </div>

      <div css={{ paddingTop: spacing.normal, paddingBottom: spacing.normal }}>
        Number of Projects: {count}
      </div>

      <div
        css={{
          display: 'grid',
          gap: spacing.spacious,
          gridTemplateColumns: `repeat(auto-fill, minmax(${MIN_WIDTH}px, 1fr))`,
          paddingBottom: spacing.spacious,
        }}
      >
        {projects
          .filter(({ name }) =>
            name.toLowerCase().includes(searchString.toLowerCase()),
          )
          .sort((a, b) => {
            if (a.name < b.name) return -1
            if (a.name > b.name) return 1
            return 0
          })
          .map(({ id: projectId, name }) => (
            <AccountCard key={projectId} projectId={projectId} name={name} />
          ))}
      </div>
    </>
  )
}

export default AccountContent
