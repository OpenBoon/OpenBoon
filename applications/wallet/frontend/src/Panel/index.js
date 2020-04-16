import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, typography, constants } from '../Styles'

import PausedSvg from '../Icons/paused.svg'

import Button, { VARIANTS } from '../Button'
import Resizeable from '../Resizeable'

const MIN_WIDTH = 400
const ICON_WIDTH = 20

const Panel = ({ openToThe, children }) => {
  const [openPanel, setOpenPanel] = useState(null)
  const panel = children[openPanel]

  return (
    <div
      css={{
        display: 'flex',
        marginTop: spacing.hairline,
        boxShadow: constants.boxShadows.leftPanel,
      }}
    >
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          marginRight: spacing.hairline,
        }}
      >
        {Object.entries(children).map(([key, { title, icon }]) => (
          <Button
            key={title}
            aria-label={title}
            variant={VARIANTS.NEUTRAL}
            onClick={() => setOpenPanel((oP) => (oP === key ? null : key))}
            isDisabled={false}
            style={{
              padding: spacing.base,
              paddingTop: spacing.normal,
              paddingBottom: spacing.normal,
              backgroundColor: colors.structure.lead,
              marginBottom: spacing.hairline,
              ':hover': {
                color: colors.key.one,
                backgroundColor: colors.structure.mattGrey,
              },
            }}
          >
            {icon}
          </Button>
        ))}
        <div
          css={{
            flex: 1,
            backgroundColor: colors.structure.lead,
          }}
        />
      </div>
      {!!openPanel && (
        <Resizeable
          minWidth={MIN_WIDTH}
          storageName={`${openToThe}OpeningPanelWidth`}
          openToThe="right"
          onMouseUp={({ width }) => {
            if (width < MIN_WIDTH) setOpenPanel(null)
          }}
        >
          <div
            css={{
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            <div
              css={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                backgroundColor: colors.structure.lead,
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
                {panel.title}
              </h2>
              <Button
                aria-label="Close Panel"
                variant={VARIANTS.NEUTRAL}
                onClick={() => setOpenPanel(null)}
                isDisabled={false}
                style={{ ':hover': { color: colors.key.one } }}
              >
                <PausedSvg
                  width={ICON_WIDTH}
                  css={{ transform: 'rotate(180deg)' }}
                  aria-hidden
                />
              </Button>
            </div>
            {panel.content}
          </div>
        </Resizeable>
      )}
    </div>
  )
}

Panel.propTypes = {
  openToThe: PropTypes.oneOf(['left', 'right']).isRequired,
  children: PropTypes.objectOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      icon: PropTypes.node.isRequired,
      content: PropTypes.node.isRequired,
    }).isRequired,
  ).isRequired,
}

export default Panel
