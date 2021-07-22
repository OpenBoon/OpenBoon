import { useState } from 'react'
import PropTypes from 'prop-types'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const OrganizationUsersMenu = ({ organizationId, user, revalidate }) => {
  const [isRemoveModalOpen, setRemoveModalOpen] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()

                    setRemoveModalOpen(true)
                  }}
                >
                  Remove User From All Projects
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>

      {isRemoveModalOpen && (
        <Modal
          title="Remove User From All Projects"
          message={`Are you sure you want to remove "${user.firstName} ${user.lastName}" from all projects in this organization? This cannot be undone.`}
          action="Remove User"
          onCancel={() => {
            setRemoveModalOpen(false)
          }}
          onConfirm={async () => {
            setRemoveModalOpen(false)

            await fetcher(
              `/api/v1/organizations/${organizationId}/users/${user.id}/`,
              {
                method: 'DELETE',
              },
            )

            revalidate()
          }}
        />
      )}
    </>
  )
}

OrganizationUsersMenu.propTypes = {
  organizationId: PropTypes.string.isRequired,
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
    projectCount: PropTypes.number.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationUsersMenu
