import { useRouter } from 'next/router'
import PropTypes from 'prop-types'
import JSONPretty from 'react-json-pretty'

import { colors, constants, spacing, typography } from '../Styles'

import InformationSvg from './information.svg'

import assetShape from '../Asset/shape'

export const WIDTH = 400

const VisualizerMetadata = ({ assets }) => {
  const {
    query: { id },
  } = useRouter()

  const asset = !!id && assets.find(({ id: assetId }) => assetId === id)

  const { metadata: { source: { filename } = {} } = {} } = asset || {}

  return (
    <div
      css={{
        backgroundColor: colors.structure.mattGrey,
        marginTop: spacing.hairline,
        height: '100%',
        width: WIDTH,
        display: 'flex',
        flexDirection: 'column',
        boxShadow: constants.boxShadows.metadata,
      }}>
      <div
        css={{
          display: 'flex',
          height: constants.navbar.height,
          alignItems: 'center',
          borderBottom: constants.borders.divider,
          padding: spacing.normal,
        }}>
        <InformationSvg width={20} color={colors.structure.steel} />
        <div
          css={{
            padding: spacing.normal,
            color: colors.structure.steel,
            fontWeight: typography.weight.bold,
          }}>
          ASSET METADATA
        </div>
      </div>
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            color: filename ? colors.signal.sky.base : colors.key.one,
            fontStyle: filename ? '' : typography.style.italic,
          }}>
          {filename || 'Select an asset to view its metadata'}
        </div>
      </div>
      <div css={{ overflow: 'auto' }}>
        <JSONPretty
          id="json-pretty"
          data={asset}
          theme={{
            main: `background:${colors.structure.coal};height:100%;margin:0;line-height:1.3;overflow:auto;`,
            string: `color:${colors.signal.grass.base};`,
            value: `color:${colors.signal.sky.base};`,
            boolean: `color:${colors.signal.canary.base};`,
          }}
        />
      </div>
    </div>
  )
}

VisualizerMetadata.propTypes = {
  assets: PropTypes.arrayOf(assetShape).isRequired,
}

export default VisualizerMetadata
