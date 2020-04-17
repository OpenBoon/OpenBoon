import assetShape from '../Asset/shape'

import MetadataSection from './Section'

const MetadataPretty = ({
  asset: {
    metadata: { system, source, media, clip },
  },
}) => {
  return (
    <div css={{ overflow: 'auto' }}>
      <MetadataSection title="System" metadata={system} />
      <MetadataSection title="Source" metadata={source} />
      <MetadataSection title="Media" metadata={media} />
      <MetadataSection title="Clip" metadata={clip} />
    </div>
  )
}

MetadataPretty.propTypes = {
  asset: assetShape.isRequired,
}

export default MetadataPretty
