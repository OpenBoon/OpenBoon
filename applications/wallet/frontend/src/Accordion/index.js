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
            paddingTop: spacing.normal,
            paddingBottom: spacing.normal,
            paddingLeft: spacing.moderate,
            display: 'flex',
          }}
        >
          <Button
            aria-label={`${isOpen ? 'Collapse' : 'Expand'} Section`}
            variant={VARIANTS.NEUTRAL}
            onClick={() => setOpen(!isOpen)}
          >
            <ChevronSvg
              width={CHEVRON_WIDTH}
              color={colors.structure.steel}
              css={{
                transform: isOpen ? 'rotate(-180deg)' : '',
                ':hover': {
                  color: colors.structure.white,
                },
              }}
            />
          </Button>
          <h4
            css={{
              fontSize: typography.size.medium,
              lineHeight: typography.height.medium,
              paddingLeft: spacing.moderate,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {title}
          </h4>
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
