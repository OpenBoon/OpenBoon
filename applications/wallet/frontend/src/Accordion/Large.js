/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { typography, colors, spacing } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

const CHEVRON_WIDTH = 20

const AccordionLarge = ({ title, cacheKey, children, isInitiallyOpen }) => {
  const [isOpen, setOpen] = useLocalStorageState({
    key: cacheKey,
    initialValue: isInitiallyOpen,
  })

  const toggle = () => setOpen({ value: !isOpen })

  return (
    <div css={{}}>
      <div css={{}} onClick={toggle}>
        <Button
          aria-label={`${isOpen ? 'Collapse' : 'Expand'} Section`}
          variant={BUTTON_VARIANTS.NEUTRAL}
          onClick={toggle}
        >
          <ChevronSvg
            width={CHEVRON_WIDTH}
            color={colors.structure.steel}
            css={{
              transform: isOpen ? 'rotate(-180deg)' : '',
              ':hover': { color: colors.structure.white },
            }}
          />
        </Button>

        <h4
          css={{
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            paddingLeft: spacing.moderate,
            display: 'flex',
          }}
        >
          {title}
        </h4>
      </div>

      {isOpen && <div css={{}}>{children}</div>}
    </div>
  )
}

AccordionLarge.propTypes = {
  title: PropTypes.node.isRequired,
  cacheKey: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
  isInitiallyOpen: PropTypes.bool.isRequired,
}

export default AccordionLarge
