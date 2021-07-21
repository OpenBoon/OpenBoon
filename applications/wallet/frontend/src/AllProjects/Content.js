import { useState } from 'react'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import NoProject from '../NoProject'
import PageTitle from '../PageTitle'
import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'
import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'

import AllProjectsCard, { LINKS } from './Card'

const MIN_WIDTH =
  // container padding
  spacing.comfy * 2 +
  // icon size
  (constants.icons.regular +
    // icon padding
    spacing.base * 2 +
    // icon gap
    spacing.normal) *
    // icon count
    Object.keys(LINKS).length -
  // off by one gap
  spacing.normal

const AllProjectsContent = () => {
  const {
    data: { results: projects },
  } = useSWR('/api/v1/projects/')

  const [searchString, setSearchString] = useState('')

  const [sortBy, setSortBy] = useLocalStorage({
    key: 'AllProjectsContent.sortBy',
    initialState: 'name',
  })

  if (!projects || projects.length === 0) {
    return <NoProject />
  }

  const sortedFilteredProjects = [...projects]
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
          if (a.name.toLowerCase() < b.name.toLowerCase()) return -1
          if (a.name.toLowerCase() > b.name.toLowerCase()) return 1
          return 0
        }
      }
    })

  return (
    <>
      <PageTitle>All Projects</PageTitle>
      <div
        css={{
          display: 'flex',
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}
      >
        <div css={{ flex: 1, maxWidth: MIN_WIDTH }}>
          <InputSearch
            aria-label="Filter Projects"
            placeholder="Filter Projects"
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
        Projects: {sortedFilteredProjects.length}
      </div>

      <div
        css={{
          display: 'grid',
          gap: spacing.spacious,
          gridTemplateColumns: `repeat(auto-fill, minmax(${MIN_WIDTH}px, 1fr))`,
          paddingBottom: spacing.spacious,
        }}
      >
        {sortedFilteredProjects.map(
          ({ id: projectId, name, organizationName }) => (
            <AllProjectsCard
              key={projectId}
              projectId={projectId}
              name={name}
              organizationName={organizationName}
            />
          ),
        )}
      </div>
    </>
  )
}

export default AllProjectsContent
