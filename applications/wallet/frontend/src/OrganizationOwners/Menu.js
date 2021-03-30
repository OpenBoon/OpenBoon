import { useState } from 'react'
import PropTypes from 'prop-types'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const OrganizationOwnersMenu = ({ organizationId, ownerId, revalidate }) => {
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
                  Remove Owner
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
      {isRemoveModalOpen && (
        <Modal
          title="Remove Owner from Organization"
          message="This owner will be removed from the organization admin, but will retain access to any projects previously added to."
          action="Remove Owner"
          onCancel={() => {
            setRemoveModalOpen(false)
          }}
          onConfirm={async () => {
            setRemoveModalOpen(false)

            await fetcher(
              `/api/v1/organizations/${organizationId}/owners/${ownerId}/`,
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

OrganizationOwnersMenu.propTypes = {
  organizationId: PropTypes.string.isRequired,
  ownerId: PropTypes.number.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationOwnersMenu
