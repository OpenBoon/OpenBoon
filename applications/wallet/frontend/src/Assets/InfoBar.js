import { constants, colors, spacing, zIndex } from '../Styles'

const BOX_SHADOW = '0px 3px 3px 0 rgb(0, 0, 0, 0.3)'

const AssetsInfoBar = () => {
  // Temporary values
  const displayCount = 100
  const totalCount = 2400

  return (
    <div
      style={{
        position: 'fixed',
        top: constants.navbar.height + 1,
        left: 0,
        right: 0,
        height: constants.navbar.height,
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'center',
        backgroundColor: colors.structure.mattGrey,
        boxShadow: BOX_SHADOW,
        zIndex: zIndex.layout.navbar,
        paddingLeft: spacing.normal,
        paddingRight: spacing.normal,
        fontFamily: 'Roboto Condensed',
      }}>
      <div css={{ color: colors.structure.steel }}>
        Sort: Import Date &nbsp;| &nbsp;
      </div>
      <div css={{ color: colors.structure.zinc }}>
        {displayCount} of {totalCount} Results
      </div>
    </div>
  )
}

export default AssetsInfoBar
