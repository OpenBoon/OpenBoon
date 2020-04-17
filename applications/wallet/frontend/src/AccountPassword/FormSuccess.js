import PropTypes from 'prop-types'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import { spacing } from '../Styles'

const AccountPasswordFormSuccess = ({ onReset }) => {
  return (
    <>
      <div
        css={{
          display: 'flex',
          paddingTop: spacing.base,
          paddingBottom: spacing.base,
        }}
      >
        <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
          New password saved.
        </FlashMessage>
      </div>
      <ButtonGroup>
        <Button variant={BUTTON_VARIANTS.PRIMARY} onClick={onReset}>
          Edit Password Again
        </Button>
      </ButtonGroup>
    </>
  )
}

AccountPasswordFormSuccess.propTypes = {
  onReset: PropTypes.func.isRequired,
}

export default AccountPasswordFormSuccess
