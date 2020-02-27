import { colors, constants, spacing, typography } from '../Styles'

import InformationSvg from './information.svg'

export const WIDTH = 400
const HEADER_HEIGHT = 48

const AssetsMetadata = () => (
  <div css={{ paddingTop: 1, height: '100%' }}>
    <div
      css={{
        backgroundColor: colors.structure.mattGrey,
        height: '100%',
        width: WIDTH,
        display: 'flex',
        flexDirection: 'column',
        boxShadow: constants.boxShadows.metadata,
      }}>
      <div
        css={{
          display: 'flex',
          height: HEADER_HEIGHT,
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
          css={{ color: colors.key.one, fontStyle: typography.style.italic }}>
          Select an asset to view its metadata
        </div>
      </div>
    </div>
  </div>
)

export default AssetsMetadata
