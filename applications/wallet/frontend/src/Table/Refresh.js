import { useState } from 'react'
import PropTypes from 'prop-types'
import { keyframes } from '@emotion/core'

import RefreshSvg from '../Icons/refresh.svg'

import { colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

const rotate = keyframes`
  from { transform:rotate(0deg); }
  to { transform:rotate(360deg); }
`

const TableRefresh = ({ onClick, legend }) => {
  const [clicked, setClicked] = useState(false)

  return (
    <Button
      variant={VARIANTS.PRIMARY_SMALL}
      onClick={() => {
        if (clicked) return

        setClicked(true)
        onClick()
        setTimeout(() => setClicked(false), 2000)
      }}
    >
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <RefreshSvg
          width={20}
          color={colors.structure.white}
          css={{ animation: clicked ? `${rotate} 1s linear 2` : '' }}
        />
        <div css={{ paddingLeft: spacing.small }}>Refresh {legend}</div>
      </div>
    </Button>
  )
}

TableRefresh.propTypes = {
  onClick: PropTypes.func.isRequired,
  legend: PropTypes.string.isRequired,
}

export default TableRefresh
