import { useState } from 'react'
import useSWR from 'swr'
import { useRouter } from 'next/router'

import { decode } from './helpers'

import FiltersContent from './Content'
import FiltersMenu from './Menu'

const Filters = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  const {
    query: { projectId, id: assetId = '', query },
  } = useRouter()

  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const filters = decode({ query })

  return isMenuOpen ? (
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
  )
}

export default Filters
