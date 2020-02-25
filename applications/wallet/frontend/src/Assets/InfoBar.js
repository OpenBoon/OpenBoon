import NavbarWrapper from '../Navbar/Wrapper'

import { constants, colors } from '../Styles'

const BOX_SHADOW = '0px 3px 3px 0 rgb(0, 0, 0, 0.3)'

const AssetInfoBar = () => {
  // Temporary values
  const displayCount = 100
  const totalCount = 2400

  return (
    <NavbarWrapper
      style={{
        top: constants.navbar.height + 1,
        justifyContent: 'flex-end',
        boxShadow: BOX_SHADOW,
      }}>
      <div css={{ color: colors.structure.steel }}>
        Sort: Import Date &nbsp;| &nbsp;
      </div>
      <div css={{ color: colors.structure.zinc }}>
        {displayCount} of {totalCount} Results
      </div>
    </NavbarWrapper>
  )
}

export default AssetInfoBar
