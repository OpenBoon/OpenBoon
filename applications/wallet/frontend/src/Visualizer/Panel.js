import { useState } from 'react'

import { colors, spacing, typography, constants } from '../Styles'

import AccountDashboardSvg from '../Icons/accountDashboard.svg'
import PausedSvg from '../Icons/paused.svg'

import Button, { VARIANTS } from '../Button'
import Resizeable from '../Resizeable'

const MIN_WIDTH = 400
const ICON_WIDTH = 20

const VisualizerPanel = () => {
  const [isPanelOpen, setPanelOpen] = useState(false)

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
        <Button
          aria-label="Filters"
          variant={VARIANTS.NEUTRAL}
          onClick={() => setPanelOpen((isOpen) => !isOpen)}
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
          <AccountDashboardSvg width={ICON_WIDTH} aria-hidden />
        </Button>
        <div
          css={{
            flex: 1,
            backgroundColor: colors.structure.lead,
          }}
        />
      </div>
      {isPanelOpen && (
        <Resizeable
          minWidth={MIN_WIDTH}
          storageName="leftPanelWidth"
          position="left"
          onMouseUp={({ width }) => {
            if (width < MIN_WIDTH) setPanelOpen(false)
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
                Filter
              </h2>
              <Button
                aria-label="Close Panel"
                variant={VARIANTS.NEUTRAL}
                onClick={() => setPanelOpen((isOpen) => !isOpen)}
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
          </div>
        </Resizeable>
      )}
    </div>
  )
}

export default VisualizerPanel
