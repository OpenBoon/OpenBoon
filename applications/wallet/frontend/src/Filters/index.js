import { useState } from 'react'
import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors } from '../Styles'

import FiltersContent from './Content'
import FiltersMenu from './Menu'

const Filters = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(true)

  const {
    query: { projectId, id: assetId = '', filters: f = '[]' },
  } = useRouter()

  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const filters = JSON.parse(f || '[]')

  return (
    <div
      css={{
        flex: 1,
        backgroundColor: colors.structure.lead,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      {isMenuOpen ? (
        <FiltersMenu
          projectId={projectId}
          assetId={assetId}
          filters={filters}
          fields={fields}
          setIsMenuOpen={setIsMenuOpen}
        />
      ) : (
        <FiltersContent
          projectId={projectId}
          assetId={assetId}
          filters={filters}
          setIsMenuOpen={setIsMenuOpen}
        />
      )}
    </div>
  )
}

export default Filters
