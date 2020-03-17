import { useState } from 'react'
import PropTypes from 'prop-types'

import RefreshSvg from './refresh.svg'
import RefreshingSvg from './refreshing.svg'

import { colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

const REFRESH_HEIGHT = 32

const Refresh = ({ onClick, children }) => {
  const [clicked, setClicked] = useState(false)

  const toggleClicked = () => setClicked(!clicked)

  return (
    <Button
      variant={VARIANTS.PRIMARY}
      style={{
        height: REFRESH_HEIGHT,
      }}
      onClick={() => {
        toggleClicked()
        onClick()
        setTimeout(toggleClicked, 2000)
      }}>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
        }}>
        {!clicked && <RefreshSvg width={20} color={colors.structure.white} />}
        {clicked && <RefreshingSvg width={20} color={colors.structure.white} />}
        <div css={{ paddingLeft: spacing.small }}>{children}</div>
      </div>
    </Button>
  )
}

Refresh.propTypes = {
  onClick: PropTypes.func.isRequired,
  children: PropTypes.string.isRequired,
}

export default Refresh
