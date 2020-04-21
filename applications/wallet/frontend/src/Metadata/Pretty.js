import assetShape from '../Asset/shape'

import MetadataSection from './Section'
import { formatDisplayName } from './helpers'

const MetadataPretty = ({ asset: { metadata } }) => {
  return (
    <div css={{ overflow: 'auto' }}>
      {Object.keys(metadata).map((section) => {
        return (
          <MetadataSection
            key={section}
            title={formatDisplayName({ name: section })}
          >
            {metadata[section]}
          </MetadataSection>
        )
      })}
    </div>
  )
}

MetadataPretty.propTypes = {
  asset: assetShape.isRequired,
}

export default MetadataPretty
