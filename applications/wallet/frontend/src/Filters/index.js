import { useState } from 'react'
import { useRouter } from 'next/router'

import SuspenseBoundary from '../SuspenseBoundary'

import { decode } from './helpers'

import FiltersContent from './Content'
import FiltersMenu from './Menu'

const Filters = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  const {
    pathname,
    query: { projectId, assetId = '', query },
  } = useRouter()

  const filters = decode({ query })

  return isMenuOpen ? (
    <SuspenseBoundary key={assetId}>
      <FiltersMenu
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        setIsMenuOpen={setIsMenuOpen}
      />
    </SuspenseBoundary>
  ) : (
    <SuspenseBoundary key={assetId}>
      <FiltersContent
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        setIsMenuOpen={setIsMenuOpen}
      />
    </SuspenseBoundary>
  )
}

export default Filters
