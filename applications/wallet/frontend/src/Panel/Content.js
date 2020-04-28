import PropTypes from 'prop-types'

import { colors, spacing, typography, constants } from '../Styles'

import PausedSvg from '../Icons/paused.svg'

import Button, { VARIANTS } from '../Button'
import Resizeable from '../Resizeable'

const MIN_WIDTH = 400
const ICON_WIDTH = 20

const PanelContent = ({
  openToThe,
  panel: { title, content },
  setOpenPanel,
}) => {
  return (
    <Resizeable
      minWidth={MIN_WIDTH}
      storageName={`${openToThe}OpeningPanelWidth`}
      openToThe={openToThe}
      onMouseUp={({ width }) => {
        if (width < MIN_WIDTH) setOpenPanel({ value: '' })
      }}
    >
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          height: '100%',
          overflow: 'hidden',
          backgroundColor: colors.structure.lead,
        }}
      >
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: spacing.base,
            borderBottom: constants.borders.divider,
          }}
        >
          <h2
            css={{
              textTransform: 'uppercase',
              fontWeight: typography.weight.medium,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
            }}
          >
            {title}
          </h2>
          <Button
            aria-label="Close Panel"
            variant={VARIANTS.NEUTRAL}
            onClick={() => setOpenPanel({ value: '' })}
            isDisabled={false}
            style={{
              color: colors.structure.steel,
              ':hover': { color: colors.structure.white },
            }}
          >
            <PausedSvg
              width={ICON_WIDTH}
              css={{
                transform: `rotate(${openToThe === 'left' ? 0 : 180}deg)`,
              }}
              aria-hidden
            />
          </Button>
        </div>
        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          {content}
        </div>
      </div>
    </Resizeable>
  )
}

PanelContent.propTypes = {
  openToThe: PropTypes.oneOf(['left', 'right']).isRequired,
  panel: PropTypes.shape({
    title: PropTypes.string.isRequired,
    icon: PropTypes.node.isRequired,
    content: PropTypes.node.isRequired,
  }).isRequired,
  setOpenPanel: PropTypes.func.isRequired,
}

export default PanelContent
