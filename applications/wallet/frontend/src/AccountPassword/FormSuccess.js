import PropTypes from 'prop-types'

import FormSuccess from '../FormSuccess'
import Button, { VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

const AccountPasswordFormSuccess = ({ onReset }) => {
  return (
    <div>
      <FormSuccess>New Password Saved!</FormSuccess>
      <ButtonGroup>
        <Button variant={VARIANTS.PRIMARY} onClick={onReset}>
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
