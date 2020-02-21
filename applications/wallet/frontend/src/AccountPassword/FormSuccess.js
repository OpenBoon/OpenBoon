import PropTypes from 'prop-types'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

const AccountPasswordFormSuccess = ({ onReset }) => {
  return (
    <div>
      <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
        New Password Saved
      </FlashMessage>
      <ButtonGroup>
        <Button variant={BUTTON_VARIANTS.PRIMARY} onClick={onReset}>
          Edit Password Again
        </Button>
      </ButtonGroup>
    </div>
  )
}

AccountPasswordFormSuccess.propTypes = {
  onReset: PropTypes.func.isRequired,
}

export default AccountPasswordFormSuccess
