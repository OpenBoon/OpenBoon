import { useState } from 'react'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import NoOrganization from '../NoOrganization'
import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'
import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'

import OrganizationsCard from './Card'

const MIN_WIDTH = 400

const OrganizationsContent = () => {
  const {
    data: { results: organizations },
  } = useSWR('/api/v1/organizations/')

  const [searchString, setSearchString] = useState('')

  const [sortBy, setSortBy] = useLocalStorage({
    key: 'OrganizationsContent.sortBy',
    initialState: 'name',
  })

  if (organizations.length === 0) {
    return <NoOrganization />
  }

  const sortedFilteredOrganizations = organizations
    .filter(({ name }) => {
      return name.toLowerCase().includes(searchString.toLowerCase())
    })
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
      <div
        css={{
          display: 'flex',
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}
      >
        <div css={{ flex: 1, maxWidth: MIN_WIDTH }}>
          <InputSearch
            aria-label="Filter Organizations"
            placeholder="Filter Organizations"
            value={searchString}
            onChange={({ value }) => {
              setSearchString(value)
            }}
            variant={INPUT_SEARCH_VARIANTS.EXTRADARK}
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
        Organizations: {sortedFilteredOrganizations.length}
      </div>

      <div
        css={{
          display: 'grid',
          gap: spacing.spacious,
          gridTemplateColumns: `repeat(auto-fill, minmax(${MIN_WIDTH}px, 1fr))`,
          paddingBottom: spacing.spacious,
        }}
      >
        {sortedFilteredOrganizations.map((organization) => (
          <OrganizationsCard
            key={organization.id}
            organization={organization}
          />
        ))}
      </div>
    </>
  )
}

export default OrganizationsContent
