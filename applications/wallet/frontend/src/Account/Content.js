import { useState } from 'react'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import NoProject from '../NoProject'
import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'
import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'

import AccountCard from './Card'

const MIN_WIDTH = 400

const AccountContent = () => {
  const {
    data: { results: projects },
  } = useSWR('/api/v1/projects/')

  const [searchString, setSearchString] = useState('')

  const [sortBy, setSortBy] = useLocalStorage({
    key: 'AccountContent.sortBy',
    initialState: 'name',
  })

  if (projects.length === 0) {
    return <NoProject />
  }

  const sortedFilteredProjects = projects
    .filter(({ name }) =>
      name.toLowerCase().includes(searchString.toLowerCase()),
    )
    .sort((a, b) => {
      switch (sortBy) {
        case 'date': {
          if (a.createdDate > b.createdDate) return -1
          if (a.createdDate < b.createdDate) return 1
          return 0
        }

        case 'name':
        default: {
          if (a.name < b.name) return -1
          if (a.name > b.name) return 1
          return 0
        }
      }
    })

  return (
    <>
      <div css={{ display: 'flex' }}>
        <div css={{ flex: 1, maxWidth: MIN_WIDTH }}>
          <InputSearch
            aria-label="Filter Projects"
            placeholder="Filter Projects"
            value={searchString}
            onChange={({ value }) => {
              setSearchString(value)
            }}
            variant={INPUT_SEARCH_VARIANTS.DARK}
            style={{ backgroundColor: colors.structure.smoke }}
          />
        </div>

        <div css={{ paddingLeft: spacing.normal }}>
          <Select
            label="Sort by"
            defaultValue={sortBy}
            options={[
              { value: 'name', label: 'Name (A-Z)' },
              { value: 'date', label: 'Most Recently Created' },
            ]}
            onChange={({ value }) => {
              setSortBy({ value })
            }}
            variant={SELECT_VARIANTS.ROW}
            isRequired={false}
            style={{ backgroundColor: colors.structure.smoke }}
          />
        </div>
      </div>

      <div css={{ paddingTop: spacing.normal, paddingBottom: spacing.normal }}>
        Number of Projects: {sortedFilteredProjects.length}
      </div>

      <div
        css={{
          display: 'grid',
          gap: spacing.spacious,
          gridTemplateColumns: `repeat(auto-fill, minmax(${MIN_WIDTH}px, 1fr))`,
          paddingBottom: spacing.spacious,
        }}
      >
        {sortedFilteredProjects.map(({ id: projectId, name }) => (
          <AccountCard key={projectId} projectId={projectId} name={name} />
        ))}
      </div>
    </>
  )
}

export default AccountContent
