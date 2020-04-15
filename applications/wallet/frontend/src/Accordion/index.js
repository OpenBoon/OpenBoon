import { useState } from 'react'
import PropTypes from 'prop-types'

import { typography, colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

const CHEVRON_WIDTH = 20

const Accordion = ({ title, children, isInitiallyOpen }) => {
  const [isOpen, setOpen] = useState(isInitiallyOpen)

  return (
    <div css={{ paddingTop: spacing.normal }}>
      <div
        css={{
          backgroundColor: colors.structure.smoke,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
        }}
      >
        <div
          css={{
            borderBottom: constants.borders.tabs,
            padding: spacing.normal,
            display: 'flex',
            justifyContent: 'space-between',
          }}
        >
          <h4
            css={{
              fontSize: typography.size.medium,
              lineHeight: typography.height.medium,
              display: 'flex',
              flex: 1,
              alignItems: 'center',
            }}
          >
            {title}
          </h4>
          <Button
            variant={VARIANTS.ICON}
            css={{ padding: 0 }}
            onClick={() => setOpen(!isOpen)}
          >
            <ChevronSvg
              width={CHEVRON_WIDTH}
              css={{
                marginLeft: spacing.base,
                transform: `${isOpen ? 'rotate(-180deg)' : ''}`,
              }}
            />
          </Button>
        </div>
        {isOpen && (
          <div
            css={{
              padding: spacing.spacious,
              paddingTop: spacing.normal,
            }}
          >
            {children}
          </div>
        )}
      </div>
    </div>
  )
}

Accordion.propTypes = {
  title: PropTypes.node.isRequired,
  children: PropTypes.node.isRequired,
  isInitiallyOpen: PropTypes.bool.isRequired,
}

export default Accordion
