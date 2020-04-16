import { useRouter } from 'next/router'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataContent from './Content'
import MetadataSelect from './Select'

export const WIDTH = 400
export const noop = () => {}

const Metadata = () => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  return assetId ? (
    <SuspenseBoundary key={assetId}>
      <MetadataContent projectId={projectId} assetId={assetId} />
    </SuspenseBoundary>
  ) : (
    <MetadataSelect />
  )
}

export default Metadata
