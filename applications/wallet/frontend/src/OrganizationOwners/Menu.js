import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher, getQueryString } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const OrganizationOwnersMenu = ({ organizationId, owner, revalidate }) => {
  const [isRemoveModalOpen, setRemoveModalOpen] = useState(false)
  const [isRemoving, setIsRemoving] = useState(false)

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
          message={`Are you sure you want to remove "${owner.firstName} ${owner.lastName}" from this organization? They will retain access to any projects they were previously added to.`}
          action={isRemoving ? 'Removing...' : 'Remove Owner'}
          onCancel={() => {
            setRemoveModalOpen(false)
          }}
          onConfirm={async () => {
            setIsRemoving(true)

            await fetcher(
              `/api/v1/organizations/${organizationId}/owners/${owner.id}/`,
              {
                method: 'DELETE',
              },
            )

            setRemoveModalOpen(false)

            revalidate()

            const queryString = getQueryString({
              action: 'remove-owner-success',
            })

            Router.push(
              `/organizations/[organizationId]/owners${queryString}`,
              `/organizations/${organizationId}/owners`,
            )
          }}
        />
      )}
    </>
  )
}

OrganizationOwnersMenu.propTypes = {
  organizationId: PropTypes.string.isRequired,
  owner: PropTypes.shape({
    id: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationOwnersMenu
