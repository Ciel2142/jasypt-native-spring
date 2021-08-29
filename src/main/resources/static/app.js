`strict`

const encTypeEl = document.querySelector("#selectedOption");
const resultStringEl = document.querySelector("#encryptedResultantString");
const inputStringEl = document.querySelector("#inputString");

document.querySelector("#encryptButton").addEventListener("click", function () {
    fetch(`/jacypt/api/v1/encrypt?encType=${encTypeEl.value}&value=${inputStringEl.value}`, {
        method: 'POST',
        credentials: 'same-origin',
        cache: 'no-cache'
    }).then(res => {
        if (res.ok) res.text().then(res => {
            resultStringEl.value = res;
            resultStringEl.textContent = res;
        })
        else res.json().then(res => console.log(res));
    })
})